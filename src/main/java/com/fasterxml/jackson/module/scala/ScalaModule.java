package com.fasterxml.jackson.module.scala;

import com.fasterxml.jackson.module.scala.DefaultScalaModule;

/**
 * @deprecated Use {@link com.fasterxml.jackson.module.scala.DefaultScalaModule}
 */
@Deprecated
public class ScalaModule extends DefaultScalaModule
{
    private static final String NAME = "ScalaModule";
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    
    public ScalaModule() { }

    @Override public String getModuleName() { return NAME; }
}
