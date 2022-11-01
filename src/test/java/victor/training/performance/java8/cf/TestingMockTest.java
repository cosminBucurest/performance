package victor.training.performance.java8.cf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.Charset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(MockitoExtension.class)
public class TestingMockTest {
    public static final String ID = "1";

    @InjectMocks
    Testing testing;
    @Mock
    TestingDependency dependencyMock;

    @Test
    void aNotFound() throws Exception {
        HttpClientErrorException notFound = HttpClientErrorException.create(HttpStatus.NOT_FOUND,
                "Not Found", new HttpHeaders(), null, Charset.defaultCharset());
        // TODO setup mocks

        XY result =  testing.methodToTest(ID).get();

        assertThat(result).isEqualTo(new XY(new X("Not Found"), new Y("b")));
    }

    @Test
    void aTimeout() throws Exception {
        fail("TODO");
    }

    @Test
    void aSolo() throws Exception {
        fail("TODO");
    }

    @Test
    void aAndB() throws Exception {
        fail("TODO");
    }
}
