/** Encryption: first ROT13, then reversal. */
public final class Crypto {
    private Crypto() { }

    private static String rot13(String text) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 'a' && c <= 'z') {
                c = (char) ('a' + (c - 'a' + 13) % 26);
            } else if (c >= 'A' && c <= 'Z') {
                c = (char) ('A' + (c - 'A' + 13) % 26);
            }
            result.append(c);
        }
        return result.toString();
    }

    public static String encrypt(String text, String... type) {
        if (type.equals("rot13")){
            return new StringBuffer(rot13(text)).toString();
        } else if (type.equals("reversal")){
            return new StringBuffer(text).reverse().toString();
        }
        return new StringBuffer(rot13(text)).reverse().toString(); 
    }

    public static String decrypt(String text, String... type) {
        if (type.equals("rot13")){
            return new StringBuffer(rot13(text)).toString();
        } else if (type.equals("reversal")){
            return new StringBuffer(text).reverse().toString();
        }
        return rot13(new StringBuffer(text).reverse().toString());
    }
}
