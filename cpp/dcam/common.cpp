
#include	"console4.h"
#include	"common.h"

#include	<stdarg.h>

#ifndef ASSERT
#define	ASSERT(c)
#endif

// ----------------------------------------------------------------

inline const int my_dcamdev_string(DCAMERR& err, HDCAM hdcam, int32 idStr, char* text, int32 textbytes)
{
	DCAMDEV_STRING	param;
	memset(&param, 0, sizeof(param));
	param.size = sizeof(param);
	param.text = text;
	param.textbytes = textbytes;
	param.iString = idStr;

	err = dcamdev_getstring(hdcam, &param);
	return !failed(err);
}

// ----------------------------------------------------------------

void dcamcon_show_dcamerr(HDCAM hdcam, DCAMERR errid, const char* apiname, const char* fmt, ...)
{
	char errtext[256];

	DCAMERR err;
	my_dcamdev_string(err, hdcam, errid, errtext, sizeof(errtext));

	printf("FAILED: (DCAMERR)0x%08X %s @ %s", errid, errtext, apiname);

	if (fmt != NULL)
	{
		printf(" : ");

		va_list	arg;
		va_start(arg, fmt);
		vprintf(fmt, arg);
		va_end(arg);
	}

	printf("\n");
}

// ----------------------------------------------------------------

void dcamcon_show_dcamdev_info(HDCAM hdcam)
{
	char	model[256];
	char	cameraid[64];
	char	bus[64];

	DCAMERR	err;
	if (!my_dcamdev_string(err, hdcam, DCAM_IDSTR_MODEL, model, sizeof(model)))
	{
		dcamcon_show_dcamerr(hdcam, err, "dcamdev_getstring(DCAM_IDSTR_MODEL)\n");
	}
	else
		if (!my_dcamdev_string(err, hdcam, DCAM_IDSTR_CAMERAID, cameraid, sizeof(cameraid)))
		{
			dcamcon_show_dcamerr(hdcam, err, "dcamdev_getstring(DCAM_IDSTR_CAMERAID)\n");
		}
		else
			if (!my_dcamdev_string(err, hdcam, DCAM_IDSTR_BUS, bus, sizeof(bus)))
			{
				dcamcon_show_dcamerr(hdcam, err, "dcamdev_getstring(DCAM_IDSTR_BUS)\n");
			}
			else
			{
				printf("%s (%s) on %s\n", model, cameraid, bus);
			}
}

// show HDCAM camera information by text.
void dcamcon_show_dcamdev_info_detail(HDCAM hdcam)
{
	char	buf[256];

	DCAMERR	err;
	if (!my_dcamdev_string(err, hdcam, DCAM_IDSTR_VENDOR, buf, sizeof(buf)))
		dcamcon_show_dcamerr(hdcam, err, "dcamdev_getstring(DCAM_IDSTR_VENDOR)\n");
	else
		printf("DCAM_IDSTR_VENDOR         = %s\n", buf);

	if (!my_dcamdev_string(err, hdcam, DCAM_IDSTR_MODEL, buf, sizeof(buf)))
		dcamcon_show_dcamerr(hdcam, err, "dcamdev_getstring(DCAM_IDSTR_MODEL)\n");
	else
		printf("DCAM_IDSTR_MODEL          = %s\n", buf);

	if (!my_dcamdev_string(err, hdcam, DCAM_IDSTR_CAMERAID, buf, sizeof(buf)))
		dcamcon_show_dcamerr(hdcam, err, "dcamdev_getstring(DCAM_IDSTR_CAMERAID)\n");
	else
		printf("DCAM_IDSTR_CAMERAID       = %s\n", buf);

	if (!my_dcamdev_string(err, hdcam, DCAM_IDSTR_BUS, buf, sizeof(buf)))
		dcamcon_show_dcamerr(hdcam, err, "dcamdev_getstring(DCAM_IDSTR_BUS)\n");
	else
		printf("DCAM_IDSTR_BUS            = %s\n", buf);


	if (!my_dcamdev_string(err, hdcam, DCAM_IDSTR_CAMERAVERSION, buf, sizeof(buf)))
		dcamcon_show_dcamerr(hdcam, err, "dcamdev_getstring(DCAM_IDSTR_CAMERAVERSION)\n");
	else
		printf("DCAM_IDSTR_CAMERAVERSION  = %s\n", buf);

	if (!my_dcamdev_string(err, hdcam, DCAM_IDSTR_DRIVERVERSION, buf, sizeof(buf)))
		dcamcon_show_dcamerr(hdcam, err, "dcamdev_getstring(DCAM_IDSTR_DRIVERVERSION)\n");
	else
		printf("DCAM_IDSTR_DRIVERVERSION  = %s\n", buf);

	if (!my_dcamdev_string(err, hdcam, DCAM_IDSTR_MODULEVERSION, buf, sizeof(buf)))
		dcamcon_show_dcamerr(hdcam, err, "dcamdev_getstring(DCAM_IDSTR_MODULEVERSION)\n");
	else
		printf("DCAM_IDSTR_MODULEVERSION  = %s\n", buf);

	if (!my_dcamdev_string(err, hdcam, DCAM_IDSTR_DCAMAPIVERSION, buf, sizeof(buf)))
		dcamcon_show_dcamerr(hdcam, err, "dcamdev_getstring(DCAM_IDSTR_DCAMAPIVERSION)\n");
	else
		printf("DCAM_IDSTR_DCAMAPIVERSION = %s\n", buf);
}


BOOL console_prompt(const char* prompt, char* buf, int32 bufsize)
{
	fputs(prompt, stdout);
	if (fgets(buf, bufsize, stdin) == NULL)
		return FALSE;

	return TRUE;
}


