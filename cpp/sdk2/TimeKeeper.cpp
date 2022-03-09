#include "TimeKeeper.h"
#include <windows.h>
#include <iostream>

TimeKeeper::TimeKeeper() {
	std::cout << "constructor timekeeper..." << std::endl;
}

TimeKeeper::~TimeKeeper() {
	std::cout << "destructor timekeeper..." << std::endl;
}

void TimeKeeper::setTimeStart() {
	timestart = GetTickCount();
}

double TimeKeeper::getTimeElapsed() {
	return (GetTickCount() - timestart); //ms
}