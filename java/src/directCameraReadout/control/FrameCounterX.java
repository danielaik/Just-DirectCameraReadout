package directCameraReadout.control;

public class FrameCounterX {//TODO to syncrhonize or not to synchronize

    private Object L1 = new Object();
    private Object L2 = new Object();
    private int count;

    public FrameCounterX() {
        count = 0;
    }

    public void incrementby(int inc) {
        synchronized (L1) {
            count = count + inc;
        }
    }

    public synchronized int getCount() {
        synchronized (L2) {
            return count;
        }
    }

}
