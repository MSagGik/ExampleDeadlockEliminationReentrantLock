import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ExampleDeadlockEliminationReentrantLock {
    public static void main(String[] args) throws InterruptedException {
        Runner runner = new Runner();
        // создание потоков
        Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                runner.firstThread();
            }
        });
        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                runner.secondThread();
            }
        });
        // выполнение потоков
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();
        // выполнение метода finished() после исполнения потоков
        runner.finished();
    }
}
class Runner {
    private Account account1 = new Account();
    private Account account2 = new Account();
    // объекты синхронизации потоков
    private Lock lock1 = new ReentrantLock();
    private Lock lock2 = new ReentrantLock();
    // метод залочивания
    private  void takeLocks(Lock lock1, Lock lock2) {
        boolean firstLockTaken = false;
        boolean secondLockTaken = false;
        // tryLock() пытается забрать лок,
        // если данный лок свободен, то tryLock() возвращает true
        // если данный лок занят потоком, то tryLock() возвращает false
        while (true) {
            try {
                firstLockTaken = lock1.tryLock();
                secondLockTaken = lock2.tryLock();
            } finally {
                // если оба лока свободны, то идёт возврат из данного метода
                if (firstLockTaken && secondLockTaken)
                    return;
                // если хотябы один из локов занят, то потенциально забранный лок нужно отпустить
                if (firstLockTaken)
                    lock1.unlock();
                if (secondLockTaken)
                    lock2.unlock();
            }
            // в случае неудачи взятия потоком локов нужно дать время на это другим потокам
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }
    // метод, выполняемый первым потоком
    public void firstThread() {
        Random random = new Random();
        for (int i = 0; i < 10000; i++) {
            takeLocks(lock1, lock2);
            try {
                // вымышленные переводы от account1 к account2 рандомных сумм от 0 до 99
                Account.transfer(account1, account2, random.nextInt(100));
            } finally {
                lock1.unlock();
                lock2.unlock();
            }
        }
    }
    // метод, выполняемый вторым потоком
    public void secondThread() {
        Random random = new Random();
        for (int i = 0; i < 10000; i++) {
            takeLocks(lock2, lock1);
            try {
                // вымышленные переводы от account2 к account1 рандомных сумм от 0 до 99
                Account.transfer(account2, account1, random.nextInt(100));
            } finally {
                lock1.unlock();
                lock2.unlock();
            }
        }
    }
    // метод, выполняемый после исполнения потоков
    public void finished() {
        System.out.println(account1.getBalance());
        System.out.println(account2.getBalance());
        System.out.println("Total balance " + (account1.getBalance() + account2.getBalance()));
    }
}
// моделирование операций с вымышленными счетами
class Account {
    private  int balance = 10000;
    // метод пополнения вымышленного счёта
    public void deposit(int amount) {
        balance += amount;
    }
    // метод списания с вымышленного счёта
    public void withdraw(int amount) {
        balance -= amount;
    }
    // метод просмотра вымышленного счёта
    public int getBalance() {
        return balance;
    }
    // метод перевода между вымышленными счетами
    public static void transfer(Account acc1, Account acc2, int amount) {
        acc1.withdraw(amount);
        acc2.deposit(amount);
    }
}
