package com.lc.threadpool.pool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

/**
 * 固定
 *
 * @author lc
 * @version 1.0
 * @date 2019-10-13 09:12
 **/
public class FixedSizeThreadPool {
    //思考： 如果说我们需要一个线程池需要准备什么？

    //1.需要一个仓库
    private BlockingQueue<Runnable> blockingQueue;

    //2.需要一个线程的集合
    private List<Thread> workers;

    //3.需要具体干活的线程
    public static class Worker extends Thread {

        private FixedSizeThreadPool pool;

        public Worker(FixedSizeThreadPool fixedSizeThreadPool) {
            this.pool = fixedSizeThreadPool;
        }

        @Override
        public void run() {
            //开始工作了
            while (this.pool.isWorking || !this.pool.blockingQueue.isEmpty()) {
                Runnable task = null;
                //从队列拿任务的时候，拿不到时需要阻塞
                try {
                    task = this.pool.isWorking ? this.pool.blockingQueue.poll() : this.pool.blockingQueue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (task != null) {
                    //干的具体的工作
                    task.run();
                    System.out.println("线程：" + Thread.currentThread().getName() + " 执行完毕");
                }
            }
        }
    }

    //线程池的初始化
    public FixedSizeThreadPool(int poolSize, int taskSize) {

        if (poolSize <= 0 || taskSize <= 0) {
            throw new IllegalArgumentException("非法参数");
        }

        this.blockingQueue = new LinkedBlockingQueue<>(taskSize);
        this.workers = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < poolSize; i++) {
            Worker worker = new Worker(this);
            worker.start();
            this.workers.add(worker);
        }
    }

    //把任务提交到仓库中
    public boolean submit(Runnable task) {
        return this.isWorking && this.blockingQueue.offer(task);
    }

    //关闭方法：
    //1.仓库停止接受任务
    //2.一旦仓库中还有任务，就要继续执行
    //3.拿任务时，就不该阻塞了
    //4.一旦任务已经阻塞了，我们就要中断它
    private volatile boolean isWorking = true;

    public void shutDown() {
        //执行关闭即可
        this.isWorking = false;

        workers.forEach((x) -> {
            if (x.getState().equals(Thread.State.BLOCKED)) {
                x.interrupt();
            }
        });
    }

    public static void main(String[] args) {
        FixedSizeThreadPool pool = new FixedSizeThreadPool(3, 6);
        for (int i = 0; i < 6; i++) {
            pool.submit(() -> {
                System.out.println("放入一个线程");
                try {
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException e) {
                    System.out.println("一个线程被中断：" + e.getMessage());
                }
            });
        }

        pool.shutDown();
    }
}
