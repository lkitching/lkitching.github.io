package enterprisey;

import java.io.PrintStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;

public class PrintStreamMessageSink implements MessageSink {
    private final PrintStream dest;

    public PrintStreamMessageSink(PrintStream dest) {
        this.dest = dest;
    }

    public void writeMessage(Reader message) throws IOException {
        char[] buffer = new char[1024];
        int read;

        while ((read = message.read(buffer)) != -1) {
            CharBuffer buf = CharBuffer.wrap(buffer, 0, read);
            this.dest.print(buf);
        }

        this.dest.println();
        this.dest.flush();
    }
}
