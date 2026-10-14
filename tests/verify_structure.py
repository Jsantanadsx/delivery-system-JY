#!/usr/bin/env python3
"""Offline, dependency-free requirements audit. Does not replace Gradle tests."""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
checks = {
    'Four Gradle projects': all((ROOT / n / 'build.gradle').is_file() for n in
                                ('eureka-server', 'order-service', 'payment-service', 'review-service')),
    'Gradle Java 25': 'JavaLanguageVersion.of(25)' in (ROOT / 'build.gradle').read_text(),
    'Spring Boot 4': "version '4." in (ROOT / 'build.gradle').read_text(),
    'Spring Cloud 2025.1': '2025.1.' in (ROOT / 'build.gradle').read_text(),
    'RabbitMQ Compose ports': all(x in (ROOT / 'docker-compose.yml').read_text() for x in
                                  ('rabbitmq:4-management', '5672:5672', '15672:15672')),
    'Pessimistic write lock': 'PESSIMISTIC_WRITE' in (ROOT / 'order-service/src/main/java/com/delivery/order/repository/DishRepository.java').read_text(),
    'Transactional order': '@Transactional' in (ROOT / 'order-service/src/main/java/com/delivery/order/service/OrderService.java').read_text(),
    'Eureka service discovery payment': 'http://PAYMENT-SERVICE/payments' in (ROOT / 'order-service/src/main/java/com/delivery/order/service/PaymentClient.java').read_text(),
    'Separate bean Retryable': '@Retryable' in (ROOT / 'order-service/src/main/java/com/delivery/order/service/PaymentClient.java').read_text(),
    'Payment two-port logging': 'serverPort' in (ROOT / 'payment-service/src/main/java/com/delivery/payment/PaymentController.java').read_text(),
    'RabbitMQ exchange and binding': all(x in (ROOT / 'order-service/src/main/java/com/delivery/order/config/RabbitConfig.java').read_text() for x in
                                          ('delivery.exchange', 'reviews.queue', 'reviews.new', 'TopicExchange', 'BindingBuilder')),
    'Concurrent map buffer': 'ConcurrentHashMap' in (ROOT / 'review-service/src/main/java/com/delivery/review/service/ReviewBuffer.java').read_text(),
    'Review flush every 5s': '@Scheduled(fixedDelay = 5000)' in (ROOT / 'review-service/src/main/java/com/delivery/review/service/ReviewBuffer.java').read_text(),
    'Spring AI ChatClient': 'ChatClient.Builder' in (ROOT / 'order-service/src/main/java/com/delivery/order/service/ChatService.java').read_text(),
    'No literal API secrets': not any(re.search(r'\bsk-[A-Za-z0-9]{16,}', p.read_text(errors='replace'))
                                      for p in ROOT.rglob('*') if p.is_file() and p.suffix in ('.java','.gradle','.yml','.md','.example')),
    'Concurrency test': (ROOT / 'order-service/src/test/java/com/delivery/order/OrderServiceConcurrencyTest.java').exists(),
}
if __name__ == '__main__':
    for name, result in checks.items():
        print(('PASS' if result else 'FAIL'), '-', name)
    print(f'\n{sum(checks.values())}/{len(checks)} static checks passed.')
    print('This audit does not prove compilation or integrated runtime behavior.')
    raise SystemExit(0 if all(checks.values()) else 1)
