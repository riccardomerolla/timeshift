package timeshift.entity

import java.util.{Date, UUID}

/**
 * @author Riccardo Merolla
 *         Created on 15/03/15.
 */
trait TimedDocument extends AnyRef {

  def uuid: UUID

  def dataIn: Date

  def dataOut: Date

  def source: Any
}
