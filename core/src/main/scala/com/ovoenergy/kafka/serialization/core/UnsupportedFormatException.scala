package com.ovoenergy.kafka.serialization.core

class UnsupportedFormatException(format: Format) extends RuntimeException(s"Unsupported format: $format")