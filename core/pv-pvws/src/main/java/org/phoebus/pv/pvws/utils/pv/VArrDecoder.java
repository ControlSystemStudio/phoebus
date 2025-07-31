package org.phoebus.pv.pvws.utils.pv;

import com.fasterxml.jackson.databind.JsonNode;
import org.phoebus.pv.pvws.models.pv.PvwsData;
import org.phoebus.pv.pvws.utils.Base64BufferDeserializer;

import java.util.Base64;

public class VArrDecoder {




    public static void decodeArrValue(JsonNode node, PvwsData PvData) {

        //check for encoded array field and if there is set value to decoded array.
        if (node.has("b64dbl")) {
            String base64Encoded = node.get("b64dbl").asText();
            double[] doubles = Base64BufferDeserializer.decodeDoubles(base64Encoded);
            PvData.setValue(doubles);
        }
        else if (node.has("b64flt")) {
            String base64Encoded = node.get("b64flt").asText();
            float[] floats = Base64BufferDeserializer.decodeFloats(base64Encoded);
            PvData.setValue(floats);
        }
        else if (node.has("b64int")) {
            String base64Encoded = node.get("b64int").asText();
            int[] ints = Base64BufferDeserializer.decodeInts(base64Encoded);
            PvData.setValue(ints);
        }
        else if (node.has("b64srt")) {
            String base64Encoded = node.get("b64srt").asText();
            short[] shorts = Base64BufferDeserializer.decodeShorts(base64Encoded);
            PvData.setValue(shorts);
        }
        else if (node.has("b64byt")) {
            String base64Encoded = node.get("b64byt").asText();
            byte[] bytes = Base64.getDecoder().decode(base64Encoded);
            PvData.setValue(bytes);
        }




    }

}
