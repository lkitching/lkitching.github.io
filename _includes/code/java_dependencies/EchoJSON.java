package test;

import enterprisey.*;
import java.io.Reader;
import java.io.IOException;

public class EchoJSON {
    public static void main(String[] args) {
        try {
            MessageSource source = JSONMessageSource.fromFile(args[0]);
            MessageSink sink = new PrintStreamMessageSink(System.out);

            for(Reader message : source.messages()) {
                sink.writeMessage(message);
            }
        } catch (IOException ex) {
            System.err.println("Error processing messages");
        }
    }
}
