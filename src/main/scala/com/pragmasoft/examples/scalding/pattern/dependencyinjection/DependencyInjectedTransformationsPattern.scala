package com.pragmasoft.examples.scalding.pattern.dependencyinjection

import cascading.pipe.Pipe
import com.twitter.scalding._
import com.twitter.scalding.Osv
import com.escalatesoft.subcut.inject.{BindingModule, Injectable}

object dependencyInjectedTransformationsSchema {
  val INPUT_SCHEMA = List('date, 'userid, 'url)
  val OUTPUT_SCHEMA = List('date, 'userid, 'url, 'email, 'address)
}

object BasicConversions {
  implicit def pipeToRichPipe(pipe: Pipe) : RichPipe = new RichPipe(pipe)
  implicit def richPipeToPipe(rp: RichPipe) : Pipe = rp.pipe
}

case class UserInfo(email: String, address: String)

trait ExternalService {
  def getUserInfo(userId: String) : UserInfo
}

class ExternalServiceImpl extends ExternalService {
  def getUserInfo(userId: String): UserInfo = ??? //Calls an external web service
}

trait DependencyInjectedTransformations extends FieldConversions with TupleConversions {
  import BasicConversions._

  def self: Pipe

  def externalService : ExternalService

  /** Joins with userData to add email and address
    *
    * Input schema: INPUT_SCHEMA
    * User data schema: USER_DATA_SCHEMA
    * Output schema: OUTPUT_SCHEMA */
  def addUserInfo : Pipe = self.map('userid -> ('email, 'address) ) { userId : String => externalService.getUserInfo(userId) }
}

object ConstructorInjectedTransformationsWrappers {
  implicit class ConstructorInjectedTransformationsWrapper(val self: Pipe)(implicit val externalService : ExternalService) extends DependencyInjectedTransformations
  implicit def fromRichPipe(richPipe: RichPipe)(implicit externalService : ExternalService) = new ConstructorInjectedTransformationsWrapper(richPipe.pipe)
}

class ConstructorInjectingSampleJob(args: Args) extends Job(args) {
  import ConstructorInjectedTransformationsWrappers._
  import dependencyInjectedTransformationsSchema._

  implicit val externalServiceImpl = new ExternalServiceImpl()

  Osv(args("eventsPath"), INPUT_SCHEMA).read
    .addUserInfo
    .write( Tsv(args("outputPath"), OUTPUT_SCHEMA) )
}

object FrameworkInjectedTransformationsWrappers {
  implicit class FrameworkInjectedTransformationsWrapper(val self: Pipe)(implicit val bindingModule : BindingModule) extends DependencyInjectedTransformations with Injectable {
    val externalService = inject[ExternalService]
  }
  implicit def fromRichPipe(richPipe: RichPipe)(implicit bindingModule : BindingModule) = new FrameworkInjectedTransformationsWrapper(richPipe.pipe)
}

class FrameworkInjectingSampleJob(args: Args) extends Job(args) {
  import FrameworkInjectedTransformationsWrappers._
  import dependencyInjectedTransformationsSchema._
  import com.escalatesoft.subcut.inject.NewBindingModule._

  implicit val bindingModule = newBindingModule { bindingModule =>
    import bindingModule._

    bind [ExternalService] toSingle new ExternalServiceImpl()
  }

  Osv(args("eventsPath"), INPUT_SCHEMA).read
    .addUserInfo
    .write( Tsv(args("outputPath"), OUTPUT_SCHEMA) )
}