package org.phoebus.pv.pvws.utils.pv;

public class VArrDecoder {



    /*
    public static void decodeArrValue(JsonNode node, PV pvObj) {

        //check for encoded array field and if there is set value to decoded array.
        if (node.has("b64dbl")) {
            String base64Encoded = node.get("b64dbl").asText();
            double[] doubles = Base64BufferDeserializer.decodeDoubles(base64Encoded);
            pvObj.setValue(doubles);
        }
        else if (node.has("b64flt")) {
            String base64Encoded = node.get("b64flt").asText();
            float[] floats = Base64BufferDeserializer.decodeFloats(base64Encoded);
            pvObj.setValue(floats);
        }
        else if (node.has("b64int")) {
            String base64Encoded = node.get("b64int").asText();
            int[] ints = Base64BufferDeserializer.decodeInts(base64Encoded);
            pvObj.setValue(ints);
        }
        else if (node.has("b64srt")) {
            String base64Encoded = node.get("b64srt").asText();
            short[] shorts = Base64BufferDeserializer.decodeShorts(base64Encoded);
            pvObj.setValue(shorts);
        }
        else if (node.has("b64byt")) {
            String base64Encoded = node.get("b64byt").asText();
            byte[] bytes = Base64.getDecoder().decode(base64Encoded);
            pvObj.setValue(bytes);
        }




    }
    */
}
