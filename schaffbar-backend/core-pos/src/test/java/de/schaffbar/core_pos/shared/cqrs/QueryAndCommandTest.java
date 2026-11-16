package de.schaffbar.core_pos.shared.cqrs;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttribute;

// what Spring's transaction proxy reads from the annotations – without a Spring context
class QueryAndCommandTest {

    private final AnnotationTransactionAttributeSource transactionAttributeSource = new AnnotationTransactionAttributeSource();

    @Test
    void query_isAReadOnlyTransaction() throws Exception {
        // when
        TransactionAttribute attribute = transactionAttributeOf("query");

        // then
        assertThat(attribute).isNotNull();
        assertThat(attribute.isReadOnly()).isTrue();
        assertThat(attribute.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRED);
    }

    @Test
    void command_isAWritingTransaction() throws Exception {
        // when
        TransactionAttribute attribute = transactionAttributeOf("command");

        // then
        assertThat(attribute).isNotNull();
        assertThat(attribute.isReadOnly()).isFalse();
        assertThat(attribute.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRED);
    }

    // ------------------------------------------------------------------------
    // helper

    private TransactionAttribute transactionAttributeOf(String methodName) throws NoSuchMethodException {
        Method method = AnnotatedService.class.getMethod(methodName);
        return this.transactionAttributeSource.getTransactionAttribute(method, AnnotatedService.class);
    }

    public static class AnnotatedService {

        @Query
        public void query() {}

        @Command
        public void command() {}

    }

}
