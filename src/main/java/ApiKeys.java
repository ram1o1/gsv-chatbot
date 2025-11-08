import io.github.cdimascio.dotenv.Dotenv;

public class ApiKeys {
    private static final Dotenv dotenv = Dotenv.load();
    public static final String GEMINI_API_KEY = dotenv.get("GEMINI_API_KEY");
}