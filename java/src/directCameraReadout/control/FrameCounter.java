package directCameraReadout.control;

public class FrameCounter {
    // Alternative solution is to have multiple private volatile int counter at expense of one extra JNI method call //TODO: which is better?

    private final Object L1 = new Object();
    private final Object L2 = new Object();

    private volatile int counter; // Used to keep track number of frames available to Java1Dbuffer in 1)LiveVideoWorker 2) updateTfFrameCounter() 3)SynchronizerWorker 4)NonCumulativeACFWorker
    public volatile double time1; //time taken for native runInfiniteLoop (ms)
    public volatile double time2; //average readBuffer per frame (ms)
    public volatile double time3; // average native to java heap copy (ms)

    public FrameCounter() {
        resetCounter();
    }

    private void resetCounter() {
        counter = 0;
    }

    public void increment() {
        synchronized (L1) {
            //increment counter by 1 frame every single time new counts has been transferred to javabuffer1D
            counter++;
        }
    }

    public int getCounter() { // accessed by 1)LiveVideoWorker 2) updateTfFrameCounter() 3)SynchronizerWorker 4)NonCumulativeACFWorker
        synchronized (L2) {
            return counter;
        }
    }
}
