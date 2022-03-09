#ifndef _ORCAJOB_H_
#define _ORCAJOB_H_

#include "Stoppable.h"
#include "dcamapi4.h"
#include "ImageClsSDK4.h"
#include <jni.h>
#include <assert.h>

class OrcaJob : public Stoppable
{

public:
	HDCAM m_hdcam;
	HDCAMWAIT m_hwait;
	ImageSDK4* m_ptrimgobj;
	JNIEnv** penv;
	jshortArray* poutArray;


public:

	OrcaJob();
	~OrcaJob();

	void run();
	void setter(ImageSDK4* ptrimgobj);
	void sample_wait_and_calc(HDCAM hdcam, HDCAMWAIT hwait, int32 nFrame);
	double readBufferSingle(const void* buf, int32 rowbytes, DCAM_PIXELTYPE type, int32 width, int32 height); // read buffer
	double readBufferCpyJNI(const void* buf, int32 rowbytes, DCAM_PIXELTYPE type, int32 width, int32 height, int new_c); // read buffer followed by JNI copy (sequential)
};



#endif /* _ORCAJOB_H_ */
