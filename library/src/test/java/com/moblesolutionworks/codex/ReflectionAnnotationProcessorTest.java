package com.moblesolutionworks.codex;

import com.mobilesolutionworks.codex.ActionHook;
import com.mobilesolutionworks.codex.ReflectionAnnotationProcessor;

import junit.framework.TestCase;

/**
 * Created by yunarta on 9/8/15.
 */
public class ReflectionAnnotationProcessorTest extends TestCase
{
    public static class TestActionHook
    {
        @ActionHook(name = "action")
        public void actionHook(String name, int count)
        {

        }
    }

    public static class TestActionHookB
    {
        @ActionHook(name = "action")
        public void actionHook(String name, int count)
        {

        }
    }

    public void testProcess() throws Exception
    {
        ReflectionAnnotationProcessor processor = new ReflectionAnnotationProcessor();
        processor.process(TestActionHook.class);
        processor.process(TestActionHookB.class);
    }
}