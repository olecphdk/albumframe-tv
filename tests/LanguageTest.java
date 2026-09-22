package dk.femto.albumframe;

public final class LanguageTest {
    static void check(String expected,String selection,String system){
        if(!expected.equals(UiText.resolveLanguage(selection,system)))throw new AssertionError(selection+"/"+system);
    }
    public static void main(String[] arguments){
        check("da","system","da");check("en","system","en");check("en","system","de");
        check("da","da","en");check("en","en","da");check("en","invalid","da");
        System.out.println("PASS system language, explicit override and English fallback");
    }
}
