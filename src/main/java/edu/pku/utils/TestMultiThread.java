package edu.pku.utils;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class TestMultiThread implements Runnable {
    private CountDownLatch countDownLatch; // 保存当前执行的线程数, 都是原子操作

    private ArrayList<Integer> lst;
    public ArrayList<Integer> result;
    private int now_index;

    public TestMultiThread(CountDownLatch countDownLatch) {
        lst = new ArrayList<>();
        result = new ArrayList<>();
        now_index = 0;
        for (int i = 0; i < 40; ++i) {
            lst.add(i);
        }
        this.countDownLatch = countDownLatch;
    }

    // 重载的运行方法
    @Override
    public void run() {
        Integer now = null;
        while ((now = getNext()) != null) {
            try {
                int x = (new Random()).nextInt(300);
                if (x < 0) x = 10;
                Thread.sleep(x);
            } catch (Exception e) {
                e.printStackTrace();
            }
            addResult(now * 1000 + (int) (Thread.currentThread().getId()));
        }
        this.countDownLatch.countDown();
    }

    // 同步的方法, 获得下一个处理的信息
    public synchronized Integer getNext() {
        if (now_index >= lst.size()) return null;
        return lst.get(now_index++);
    }

    // 同步的方法, 将结果放入list里面
    public synchronized void addResult(int ans) {
        result.add(ans);
    }

    public static void main(String[] args) {
        int thread_num = 3; // 线程池的个数
        CountDownLatch countDownLatch = new CountDownLatch(thread_num);
        TestMultiThread mythread = new TestMultiThread(countDownLatch);

        Thread[] threads = new Thread[thread_num];
        for (int i = 0; i < thread_num; ++i) {
            threads[i] = new Thread(mythread);
        }

        for (int i = 0; i < thread_num; ++i) {
            threads[i].start();
        }

        try {
            countDownLatch.await();
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (Integer x : mythread.result) {
            System.out.println("res: " + x);
        }
    }
}

