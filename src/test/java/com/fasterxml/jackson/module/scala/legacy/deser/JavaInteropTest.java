package com.fasterxml.jackson.module.scala.legacy.deser;

import org.junit.Test;

import java.io.IOException;

public class JavaInteropTest {

    @Test
    public void testInteropDeserialization() throws IOException {
        B v = Util.mapper().readValue(Util.jsonString(), B.class);
        assert(v.equals(new B("asdf", new A1("qwer"))));
    }

}
