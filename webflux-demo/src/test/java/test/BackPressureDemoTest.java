package test;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class BackPressureDemoTest {

    private final int EVENT_DURATION   = 10;    // 生成的事件间隔时间，单位毫秒
    private final int EVENT_COUNT      = 20;    // 生成的事件个数
    private final int PROCESS_DURATION = 30;    // 订阅者处理每个元素的时间，单位毫秒

    private Flux<String> fastPublisher;
    private SlowSubscriber slowSubscriber;
    private MyMessageProcessor<String> myMessageProcessor;
    private CountDownLatch countDownLatch;


    /**
     * 测试create方法的不同OverflowStrategy的效果。
     */
    @Test
    public void testCreateBackPressureStratety() {
        fastPublisher =
                createFlux(FluxSink.OverflowStrategy.ERROR)
                        .doOnRequest(n -> System.out.println("         ===  request: " + n + " ==="))
                        .publishOn(Schedulers.newSingle("newSingle"), 1);
    }

    /**
     * 测试不同的onBackpressureXxx方法的效果。
     */
    @Test
    public void testOnBackPressureMethod() {
        fastPublisher = createFlux(FluxSink.OverflowStrategy.BUFFER)
                .onBackpressureBuffer()     // BUFFER
//                .onBackpressureDrop()     // DROP
//                .onBackpressureLatest()   // LATEST
//                .onBackpressureError()    // ERROR
                .doOnRequest(n -> System.out.println("         ===  request: " + n + " ==="))
                .publishOn(Schedulers.newSingle("newSingle"), 1);
    }


    /**
     * 准备工作。
     */
    @Before
    public void setup() {
        countDownLatch = new CountDownLatch(1);
        slowSubscriber = new SlowSubscriber();
        myMessageProcessor = new MyMessageProcessor();
    }

    /**
     * 触发订阅，使用CountDownLatch等待订阅者处理完成。
     */
    @After
    public void subscribe() throws InterruptedException {
        fastPublisher.subscribe(slowSubscriber);
        generateEvent(EVENT_COUNT, EVENT_DURATION);
        countDownLatch.await(1, TimeUnit.MINUTES);
    }


    /**
     * 使用create方法生成“快的发布者”。
     * @param strategy 回压策略
     * @return  Flux
     */
    private Flux<String> createFlux(FluxSink.OverflowStrategy strategy) {
        return Flux.create(sink -> myMessageProcessor.register(new MyMessageListener<String>() {

            @Override
            public void onEvent(String event) {
                System.out.println("publish >>> " + event);
                sink.next(event);
            }

            @Override
            public void complete() {
                sink.complete();
            }

        }), strategy);
    }
    /**
     * 生成 Event。
     * @param count 生成Event的个数。
     * @param millis 每个Event之间的时间间隔。
     */
    private void generateEvent(int count, int millis) {
        // 循环生成MyEvent，每个MyEvent间隔millis毫秒
        for (int i = 0; i < count; i++) {
            try {
                TimeUnit.MILLISECONDS.sleep(millis);
            } catch (InterruptedException e) {
            }
            myMessageProcessor.publishEvent( "Event-" + i);
        }
        myMessageProcessor.completeEvent();
    }


    /**
     * 内部类，“慢的订阅者”。
     */
    class SlowSubscriber extends BaseSubscriber<String> {

        @Override
        protected void hookOnSubscribe(Subscription subscription) {
            request(1);     // 订阅时请求1个数据
        }

        @Override
        protected void hookOnNext(String event) {
            System.out.println("                      receive <<< " + event);
            try {
                TimeUnit.MILLISECONDS.sleep(PROCESS_DURATION);
            } catch (InterruptedException e) {
            }
            request(1);     // 每处理完1个数据，就再请求1个
        }

        @Override
        protected void hookOnError(Throwable throwable) {
            System.err.println("                      receive <<< " + throwable);
        }

        @Override
        protected void hookOnComplete() {
            countDownLatch.countDown();
        }
    }

    class  MyMessageProcessor<T>{

        private MyMessageListener<T> myMessageListener;

        /**
         * 注册监听
         * @param myMessageListener
         */
        public void register(MyMessageListener<T> myMessageListener){
                this.myMessageListener=myMessageListener;
            }

        /**
         * 发布事件
         * @param t
         */
        public void publishEvent(T t){
                myMessageListener.onEvent(t);
            }

        /**
         * 触发完成事件
         */
        public  void completeEvent(){
                myMessageListener.complete();
            }
    }

    interface MyMessageListener<T>{
        /**
         * 事件
         * @param t
         */
        void onEvent(T t);

        /**
         * 完成
          */
        void complete();
    }
}
