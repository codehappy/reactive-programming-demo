package test;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

public class ReactorDemoTest {

    @Test
    public void hello() {
        Flux.just(1, 2, 3, 4).map(i -> i * 2).subscribe(System.out::println);
    }

    @Test
    public void testScheduling() throws Exception {
        Flux.<Integer>create(i -> {
            System.out.println("" + Thread.currentThread().getName() + " create");
            i.next(1);
            i.next(2);
            i.next(3);
            i.next(4);
            i.complete();
        })
        .map(i -> {
            System.out.println("" + Thread.currentThread().getName() + " map:" + i);
            return i + 1;
        })
        .publishOn(Schedulers.newParallel("Parallel------"))
        .filter(i -> {
            System.out.println("" + Thread.currentThread().getName() + " filter:" + i);
            return i % 2 == 0;
        })
        .map(i -> {
            System.out.println("" + Thread.currentThread().getName() + " [map]:" + i);
            return 1 + 1;
        })
        .subscribeOn(Schedulers.newElastic("myElastic-------"))
        .subscribe(i -> {
            System.out.println("" + Thread.currentThread().getName() + " subscribe:" + i);
        });

        System.in.read();
    }


}
