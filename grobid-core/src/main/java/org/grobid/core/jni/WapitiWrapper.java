package org.grobid.core.jni;

import java.io.File;

import com.google.common.base.Throwables;
import fr.limsi.wapiti.SWIGTYPE_p_mdl_t;
import fr.limsi.wapiti.Wapiti;

import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.exceptions.GrobidExceptionStatus;

public class WapitiWrapper {
    public static String label(SWIGTYPE_p_mdl_t model, String data) {
        if (data.trim().isEmpty()) {
            System.err.println(
                    "Empty data is provided to Wapiti tagger: " + Throwables.getStackTraceAsString(new Throwable()));
            return "";
        }

        String result = Wapiti.labelFromModel(model, data);
        if (result == null) {
            throw new GrobidException(
                    "Wapiti tagging failed (null data returned) - Possibly mismatch between grobid-home and grobid-core",
                    GrobidExceptionStatus.TAGGING_ERROR);
        }
        return result;
    }

    public static SWIGTYPE_p_mdl_t getModel(File model) {
        return getModel(model, false);
    }

    public static SWIGTYPE_p_mdl_t getModel(File model, boolean checkLabels) {
        return Wapiti.loadModel("label " + (checkLabels ? "--check" : "") + " -m " + model.getAbsolutePath());
    }

}
