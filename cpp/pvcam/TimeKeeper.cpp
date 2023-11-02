#include "TimeKeeper.h"
#include <windows.h>
#include <iostream>

TimeKeeper::TimeKeeper() {
}

TimeKeeper::~TimeKeeper() {
}

void TimeKeeper::setTimeStart() {
	timestart = GetTickCount(); 
}

double TimeKeeper::getTimeElapsed() {
	return (GetTickCount() - timestart); //ms
}