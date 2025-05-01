public class Wait {
    public static void main(String[] args) {
        try {
            Thread.currentThread().sleep(60000);
        } catch (InterruptedException ex) {
            System.out.println("Interrupted :(");
        }

        System.out.println("Bye!");
    }
}
