package com.ovoenergy.kafka.serialization.avro;

import javax.ws.rs.client.ClientBuilder;

/**
 * The scala compiler cannot infer correctly a method, when it is overloaded by another one with varargs.
 */
public class JerseyClientRegisterHack {

    public static ClientBuilder register(ClientBuilder cb, Object feature) {
        return cb.register(feature);
    }
}
