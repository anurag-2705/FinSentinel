import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.Scanner;

public class PasswordHashGenerator {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String password = scanner.nextLine();

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

        System.out.println(encoder.encode(password));
    }
}