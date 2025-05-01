package enterprisey;

import java.io.Reader;
import java.io.IOException;

public interface MessageSink {
    void writeMessage(Reader message) throws IOException;
}
