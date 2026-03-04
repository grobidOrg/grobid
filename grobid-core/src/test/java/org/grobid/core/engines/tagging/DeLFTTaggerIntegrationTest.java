package org.grobid.core.engines.tagging;

import jep.Jep;
import jep.JepConfig;
import org.grobid.core.data.Date;
import org.grobid.core.engines.DateParser;
import org.grobid.core.engines.EngineParsers;
import org.grobid.core.jni.JEPThreadPool;
import org.grobid.core.main.LibraryLoader;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

@Ignore("Requires JEP/DeLFT environment")
public class DeLFTTaggerIntegrationTest {

    DeLFTTagger target;

    @Test
    public void setUp() throws Exception {
        LibraryLoader.load();
//        System.setProperty("java.library.path", System.getProperty("java.library.path") + ":" + LibraryLoader.getLibraryFolder());
//        System.setProperty("java.library.path", System.getProperty("java.library.path") + ":" + "/anaconda3/envs/tensorflow/lib");
//        System.setProperty("java.library.path", System.getProperty("java.library.path") + ":" + "/anaconda3/envs/tensorflow/lib/python3.6/site-packages/");

//        System.out.println(System.getProperty("java.library.path"));

//        System.loadLibrary("python3.6m");
//        System.loadLibrary("jep");

//        JepConfig config = new JepConfig();
//        config.setInteractive(false);
//        config.setClassLoader(this.getClass().getClassLoader());

//        System.out.println(LibraryLoader.getLibraryFolder());


//        Jep jep = JEPThreadPool.getInstance().getJEPInstance();
//        jep.eval("import keras");


        EngineParsers engineParsers = new EngineParsers();
        DateParser dateParser = engineParsers.getDateParser();

        List<Date> processing = dateParser.processing("23 november 2019");

        System.out.println(processing.get(0).toString());
    }

}