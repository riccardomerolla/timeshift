package timeshift

import org.slf4j.LoggerFactory

/**
 * @author Riccardo Merolla
 *         Created on 15/03/15.
 */
trait Logging {
  val logger = LoggerFactory.getLogger(getClass)
}