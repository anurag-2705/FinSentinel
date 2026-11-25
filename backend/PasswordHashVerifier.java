import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.Scanner;

public class PasswordHashVerifier {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter your development password: ");
        String password = scanner.nextLine();

        System.out.print("Paste the stored BCrypt hash: ");
        String hash = scanner.nextLine();

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

        if (encoder.matches(password, hash)) {
            System.out.println("\nRESULT: PASSWORD MATCHES");
        } else {
            System.out.println("\nRESULT: PASSWORD DOES NOT MATCH");
        }

        password = null;
        hash = null;
    }
}