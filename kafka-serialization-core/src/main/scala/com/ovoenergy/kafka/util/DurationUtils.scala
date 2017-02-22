package com.ovoenergy.kafka.util

import scala.concurrent.duration.{Duration, FiniteDuration}

trait DurationUtils {

  protected def getDuration(value: String): Duration = {
    val duration = Duration.create(value)
    if (!duration.isFinite || duration.toMillis <= 0) Duration.Inf
    else duration
  }

  protected def getFiniteDuration(value: String): FiniteDuration = {
    val duration = getDuration(value)
    if (duration.isFinite) FiniteDuration(duration.length, duration.unit)
    else throw new IllegalArgumentException(s"$value must be finite")
  }

}
