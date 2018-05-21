package io.mytc.timechain.data.serialization

trait Transcoder[From, To] extends (From => To)
