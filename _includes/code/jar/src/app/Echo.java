package test;

import enterprisey.*;
import java.io.Reader;
import java.io.IOException;

public class Echo {
    public static void main(String[] args) {
        MessageSource source = new CommandLineMessageSource(args);
        MessageSink sink = new PrintStreamMessageSink(System.out);

        try {
            for(Reader message : source.messages()) {
                sink.writeMessage(message);
            }
        } catch (IOException ex) {
            System.err.println("Error processing messages");
        }
    }
}
