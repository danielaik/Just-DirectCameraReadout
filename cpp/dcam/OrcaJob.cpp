#include "OrcaJob.h"

OrcaJob::OrcaJob() {
	m_hdcam = NULL;
	m_hwait = NULL;
}

OrcaJob::~OrcaJob() {
}

void OrcaJob::run() {

	int acqm = m_ptrimgobj->acqmode_;

	switch (acqm) {
	case 1:
		sample_wait_and_calc(m_hdcam, m_hwait, 1); //single capture
		break;
	case 2:
		sample_wait_and_calc(m_hdcam, m_hwait, m_ptrimgobj->totalframe_); // acquisition
		break;
	case 3:
		 //infinite loop
		break;
	}

	while (stopRequested() == false)
	{
		std::this_thread::sleep_for(std::chrono::milliseconds(1000));
	}
}


void OrcaJob::setter(ImageSDK4* ptrimgobj) {
	m_ptrimgobj = ptrimgobj;
}

void OrcaJob::sample_wait_and_calc(HDCAM hdcam, HDCAMWAIT hwait, int32 nFrame) {

	// wait start param
	DCAMWAIT_START	waitstart;
	memset(&waitstart, 0, sizeof(waitstart));
	waitstart.size = sizeof(waitstart);
	waitstart.eventmask = DCAMWAIT_CAPEVENT_FRAMEREADY;
	waitstart.timeout = 1000;

	// prepare frame param
	DCAMBUF_FRAME	bufframe;
	memset(&bufframe, 0, sizeof(bufframe));
	bufframe.size = sizeof(bufframe);
	bufframe.iFrame = -1;				// latest frame

	int32 i;
	int new_c;
	for (i = 0; i < nFrame; i++)
	{
		if (stopRequested() == true) {
			break;
		}
		
		new_c = i % m_ptrimgobj->size_b_;

		DCAMERR err;

		// wait image
		err = dcamwait_start(hwait, &waitstart);
		if (failed(err))
		{
			std::cout << "dcamwait_start fail" << std::endl;
			assert((false) && "dcamwait_start fail");
		}

		// transferinfo param
		DCAMCAP_TRANSFERINFO captransferinfo;
		memset(&captransferinfo, 0, sizeof(captransferinfo));
		captransferinfo.size = sizeof(captransferinfo);

		// get number of captured image
		err = dcamcap_transferinfo(hdcam, &captransferinfo);
		if (failed(err))
		{
			std::cout << "dcamcap_transferinfo failed" << std::endl;
			assert((false) && "dcamcap_transferinfo failed");
		}

		if (captransferinfo.nFrameCount < 1)
		{
			std::cout << "not capture image" << std::endl;
			assert((false) && "not capture image");
		}
		else {
			//std::cout << "frame " << i << ", new_c: " << new_c << std::endl;
			//std::cout << "pass 1 after get number of capture imagecaptransferinfo.size: " << captransferinfo.size << ", captransferinfo.nFrameCount: " << captransferinfo.nFrameCount << ", captransferinfo.iKind: " << captransferinfo.iKind << ", nNewestFrameIndex: " << captransferinfo.nNewestFrameIndex << std::endl;
		}

		// access image
		err = dcambuf_lockframe(hdcam, &bufframe);
		if (failed(err))
		{
			std::cout << "dcambuf_lockframe failed" << std::endl;
			assert((false) && "dcambuf_lockframe failed");
		}

		//a frame has come; read buffer and JNI transfer
		if (m_ptrimgobj->acqmode_ == 1) {
			int iRet = readBufferSingle(bufframe.buf, bufframe.rowbytes, bufframe.type, bufframe.width, bufframe.height);
		}
		else if (m_ptrimgobj->acqmode_ == 2) {
			int iRet = readBufferCpyJNI(bufframe.buf, bufframe.rowbytes, bufframe.type, bufframe.width, bufframe.height, new_c);
		}
		else {
			assert((false) && "acqmode 3 not yet implemented");
		}
		
	}

	if (stopRequested() == false) {
		this->stop();
	}
}


double OrcaJob::readBufferSingle(const void* buf, int32 rowbytes, DCAM_PIXELTYPE type, int32 width, int32 height) {
	if (type != DCAM_PIXELTYPE_MONO16)
	{
		// not implement
		return -1;
	}

	int32 cx = m_ptrimgobj->nopixelX_;
	int32 cy = m_ptrimgobj->nopixelY_;

	// read from buffer
	const char* src = (const char*)buf;
	const unsigned short* s = (const unsigned short*)src;
	int32 x, y;

	for (y = 0; y < cy; y++)
	{
		for (x = 0; x < cx; x++)
		{
			*(m_ptrimgobj->pImageArray1_ + (y * cx) + x) = *s++;
		}
	}

	/*
	double total = 0;
	int totala = cx * cy;
	std::cout << "total elem to be average: " << totala << std::endl;
	for (int i = 0; i < totala; i++) {
		total += *(m_ptrimgobj->pImageArray1_ + i);
	}
	total = total / totala;
	std::cout << "average: " << total << std::endl;
	*/

	return 0;
}

double OrcaJob::readBufferCpyJNI(const void* buf, int32 rowbytes, DCAM_PIXELTYPE type, int32 width, int32 height, int new_c) {

	if (type != DCAM_PIXELTYPE_MONO16)
	{
		// not implement
		return -1;
	}

	int32 cx = m_ptrimgobj->nopixelX_;
	int32 cy = m_ptrimgobj->nopixelY_;
	int32 noelemperframe = cx * cy;

	// read from buffer
	const char* src = (const char*)buf;
	const unsigned short* s = (const unsigned short*)src;
	int32 x, y;

	for (y = 0; y < cy; y++)
	{
		for (x = 0; x < cx; x++)
		{
			*(m_ptrimgobj->pImageArray1_ + (y * cx) + x) = *s++;
		}
	}

	(*penv)->SetShortArrayRegion(*poutArray, (new_c * noelemperframe), noelemperframe, m_ptrimgobj->pImageArray1_); // TODO: i previouslty use env instead of _env; Test new version if it crashes

	return 0;
}


