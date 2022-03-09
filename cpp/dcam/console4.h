
#include    <stdlib.h>
#include    <string.h>
#include    <stdint.h>

#include			"dcamapi4.h"
#include			"dcamprop.h"

// common headers

#include	<stdio.h>

// DCAM-API headers

#ifndef _NO_DCAMAPI

#ifndef DCAMAPI_VER
#define	DCAMAPI_VER		4000
#endif

#ifndef DCAMAPI_VERMIN
#define	DCAMAPI_VERMIN	4000
#endif


#endif // _NO_DCAMAPI

// ----------------------------------------------------------------

// define common macro

#ifndef ASSERT
#define	ASSERT(c)
#endif

// absorb different function

#ifdef WIN32

#if defined(UNICODE) || defined(_UNICODE)
#define	_T(str)	L##str
#else
#define	_T(str)	str
#endif

#elif defined( MACOSX ) || __ppc64__ || __i386__ || __x86_64__ || defined( LINUX )

#define	_T(str)	str

#endif

// absorb Visual Studio 2005 and later

#if ! defined(WIN32) || _MSC_VER < 1400

#define	_stricmp(str1, str2)				strncasecmp( str1, str2, strlen(str2) )
#define	sprintf_s							snprintf

#define	BOOL			int
#define	BYTE			uint8_t
#define	WORD			uint16_t
#define	DWORD			uint32_t
#define	LONGLONG		int64_t

#define	MAX_PATH		256
#define	TRUE			1
#define	FALSE			0

#define LARGE_INTEGER	int64_t

inline int fopen_s(FILE** fpp, const char* filename, const char* filemode)
{
	*fpp = fopen(filename, filemode);
	if (fpp == NULL)
		return 1;
	else
		return 0;
}





#endif

// ----------------------------------------------------------------

