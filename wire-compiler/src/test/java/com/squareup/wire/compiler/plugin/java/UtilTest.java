package com.squareup.wire.compiler.plugin.java;

import java.io.File;
import org.junit.Test;

import static com.squareup.wire.compiler.plugin.java.Util.getJavaFile;
import static com.squareup.wire.compiler.plugin.java.Util.sanitizeJavadoc;
import static com.squareup.wire.compiler.plugin.java.Util.sanitizeName;
import static org.fest.assertions.api.Assertions.assertThat;

public class UtilTest {
  @Test public void sanitizeJavadocStripsTrailingWhitespace() {
    String input = "The quick brown fox  \nJumps over  \n\t \t\nThe lazy dog  ";
    String expected = "The quick brown fox\nJumps over\n\nThe lazy dog";
    assertThat(sanitizeJavadoc(input)).isEqualTo(expected);
  }

  @Test public void sanitizeJavadocGuardsFormatCharacters() {
    String input = "This is 12% of %s%d%f%c!";
    String expected = "This is 12%% of %%s%%d%%f%%c!";
    assertThat(sanitizeJavadoc(input)).isEqualTo(expected);
  }

  @Test public void sanitizeJavadocWrapsSeeLinks() {
    String input = "Google query.\n\n@see http://google.com";
    String expected = "Google query.\n\n@see <a href=\"http://google.com\">http://google.com</a>";
    assertThat(sanitizeJavadoc(input)).isEqualTo(expected);
  }

  @Test public void nameSanitizeExamples() {
    // Just some random examples for sanity checking.
    assertThat(sanitizeName("default")).isEqualTo("_default");
    assertThat(sanitizeName("finally")).isEqualTo("_finally");
    assertThat(sanitizeName("switch")).isEqualTo("_switch");
  }

  @Test public void getJavaFileExamples() {
    assertThat(getJavaFile(new File("foo"), "bar.Baz").getPath()) //
        .isEqualTo("foo/bar/Baz.java".replace('/', File.separatorChar));
  }
}
