package fr.sirs.core.authentication;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import java.io.IOException;

/**
 *
 * @author Alexis Manin (Geomatys)
 */
public class SafePasswordDeserializer extends PasswordDeserializer {

    @Override
    public String deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        try {
        return super.deserialize(jp, ctxt);
        } catch (IOException e) {
            return jp.getText();
        }
    }
}
