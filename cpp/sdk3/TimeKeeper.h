#ifndef _TIMEKEEPER_H__
#define _TIMEKEEPER_H__

class TimeKeeper {
private:
	int timestart;
public:
	TimeKeeper();
	~TimeKeeper();
	void setTimeStart();
	double getTimeElapsed();//in milliseconds
};

#endif /* _TIMEKEEPER_H__ */
