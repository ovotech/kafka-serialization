package com.ovoenergy.kafka.serialization.core

/**
  * This exception is raised when the payload format does not match the expected one.
  */
class UnsupportedFormatException(format: Format) extends RuntimeException(s"Unsupported format: $format")
