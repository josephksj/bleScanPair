// Facebook Technologies, LLC Proprietary and Confidential.
//Create exe with command  gcc -o adb_cmd_parser adb_cmd_parser.c

#include <stddef.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <sys/stat.h>
// Headers needed for sleeping.
#ifdef _WIN32
#include <windows.h>
#else
#include <unistd.h>
#endif

int hndlr_start_app(int index, int argc, char *argv[]);
int hndlr_start_scan(int index, int argc, char *argv[]);
int hndlr_stop_scan(int index, int argc, char *argv[]);
int hndlr_ls_scan(int index, int argc, char *argv[]);
int hndlr_start_pair(int index, int argc, char *argv[]);
int hndlr_drop_pair(int index, int argc, char *argv[]);
int hndlr_ls_pair(int index, int argc, char *argv[]);
int hndlr_start_connect(int index, int argc, char *argv[]);
int hndlr_drop_connect(int index, int argc, char *argv[]);
int hndlr_indication(int index, int argc, char *argv[]);
static void print_usage();

//#define ENABLE_DBG

/*********************************************************************************
*********************************************************************************/
typedef struct cmd_lkt{
    char *cmd;
    int (*handler)(int index, int argc, char *argv[]);
    char *msg_tag;
    char *help_str;
}cmd_lkt_t;
/*********************************************************************************
*********************************************************************************/
cmd_lkt_t cmd_tbl[] = {
        {"start_app",       hndlr_start_app,      "START_APP",    "<timeout> <num_disp_lines>"},
        {"start_scan",      hndlr_start_scan,    "START_SCAN",   "<timeout> <num_disp_lines>"},
		{"stop_scan",		hndlr_stop_scan, 	  "STOP_SCAN", 	  "<timeout> <num_disp_lines>"},
		{"ls_scan",			hndlr_ls_scan, 	  "LS_SCAN", 	  "<timeout> <num_disp_lines>"},
		{"start_pair",		hndlr_start_pair, 	  "START_PAIR",   "<timeout> <num_disp_lines> <A/N> <Adrr/Name>"},
		{"drop_pair",		hndlr_drop_pair, 	  "START_PAIR", 	  "Unpair with the device"},
		{"ls_pair",			hndlr_ls_pair, 	  "LS_PAIR", 	  "Display current pair list"},
		{"start_connect", 	hndlr_start_connect,"START_CONNECT", "Start Connecting device"},
		{"drop_connect",	hndlr_drop_connect, "START_CONNECT",  "Disconnect the device"},
		{"indication",		hndlr_indication, 	 "INDICATION",	  "Start/Stop Indication"},
};


/*********************************************************************************
*********************************************************************************/
int main(int argc, char *argv[])
{
 int cmd_len = sizeof(cmd_tbl)/sizeof(cmd_lkt_t);

  if(argc < 2) {
      print_usage();
      return -1;
  }
  for(int i=0; i<cmd_len; i++) {
      if(strcmp(argv[1], cmd_tbl[i].cmd) == 0) {
          if(cmd_tbl[i].handler){
              return cmd_tbl[i].handler(i, argc, argv);
          }
          return -1;
      }
  }
  printf("Invalid Parameter ..... \n");
  print_usage();

  return -2;
}

/*********************************************************************************
*********************************************************************************/
#define	CMD_ADB_DUMP	"adb  logcat -d -s"
#define	CMD_ADB_CLEAR  "adb  logcat -c -s"
#define MODULE_NAME	"com.example.android.bluetoothlegatt"
#define	CMD_ADB_SCAN  	"adb shell \"am broadcast -a com.example.android.bluetoothlegatt.TEST_ACTION --es com.example.android.bluetoothlegatt.EXTRA_TEXT 'start_scan'\""

#define MODULE_NAME     "com.example.android.bluetoothlegatt"
#define START_ACTIVITY  "DeviceScanActivity"

#define     MSG_STR_LEN 256
#define     CMD_STR_LEN 128
#define     TAG_MATCHING        0
#define     TAG_NOT_MATCHING    -1

int clear_logcat_msg(char *msg_tag)
{
	char msg_str[MSG_STR_LEN];
	
    snprintf(msg_str, MSG_STR_LEN, "%s %s",CMD_ADB_CLEAR, msg_tag);
#ifdef ENABLE_DBG
	printf("clear_logcat_msg Command %s\n", msg_str);
#endif
	system(msg_str);
				 
	return 0;
}
/*********************************************************************************
*********************************************************************************/
int issue_system_cmd(char *cmd_str, int disp_resp_flag)
{
    FILE *fp;
    char msg_str[MSG_STR_LEN];

    if ((fp = popen(cmd_str, "r")) == NULL) {
            printf("Error opening pipe!\n");
            return -1;
    }
    while (fgets(msg_str, MSG_STR_LEN, fp) != NULL) {
        if(disp_resp_flag!=0)
            printf("OUTPUT: %s", msg_str);
    }
    pclose(fp);
    return 0;
}

/*********************************************************************************
*********************************************************************************/
char prev_msg[MSG_STR_LEN];
int check_for_tag(char *msg, char *tag)
{
    int len_to_compare;
    int tag_len;

    if((msg==NULL) || (tag==NULL))
        return TAG_NOT_MATCHING;

    tag_len = strlen(tag);
    len_to_compare = strlen(msg) - tag_len;
    if(len_to_compare<0)
        return TAG_NOT_MATCHING;
   // printf("LengthToCompare: %d \n", len_to_compare);

    if(strncmp(msg, tag, tag_len)==0)
       return TAG_MATCHING;

    for(int i=0; i<len_to_compare; i++) {
       // printf("%d) Msg: %s\n",i, msg+i);

        if(strncmp(msg+i, tag, tag_len)==0){
            if(strncmp(msg, prev_msg, MSG_STR_LEN) == 0) //Have problem with read buffer not getting cleared
                return TAG_NOT_MATCHING;
            strncpy(prev_msg, msg, MSG_STR_LEN);
           return TAG_MATCHING;
        }
    }
    return TAG_NOT_MATCHING;
}
/*********************************************************************************
*********************************************************************************/
int issue_adb_cmd_and_wait_for_response(char *cmd_str, char *parm_str, char *msg_tag, int time_to_wait, int no_lines_to_display)
{
	FILE *fp;
    char msg_str[MSG_STR_LEN];
    char adb_cmd_str[CMD_STR_LEN];
	
	clear_logcat_msg(msg_tag);
	
	if(parm_str == NULL)
	    snprintf(msg_str, MSG_STR_LEN, "adb shell \"am broadcast -a %s.TEST_ACTION --es %s.EXTRA_TEXT '%s'\"", MODULE_NAME, MODULE_NAME, cmd_str);
	else
        snprintf(msg_str, MSG_STR_LEN, "adb shell \"am broadcast -a %s.TEST_ACTION --es %s.EXTRA_TEXT '%s %s'\"", MODULE_NAME, MODULE_NAME, cmd_str, parm_str);

#ifdef ENABLE_DBG
	printf("D-issue_adb_cmd_and_wait_for_response Command %s\n", msg_str);
#endif
	issue_system_cmd(msg_str, 0);
	
	//----- Get the log cat message with the tag of interesting, wait with timeout
    snprintf(adb_cmd_str, CMD_STR_LEN, "%s %s", CMD_ADB_DUMP, msg_tag);     //"adb  logcat -d -s BLE_TEST"
#ifdef ENABLE_DBG
    printf("ADB Message Dump Command %s\n", msg_str);
#endif

    if(time_to_wait <= 0)
        time_to_wait = 1;   //Minimum one attempt
    if(no_lines_to_display <=0)
        no_lines_to_display = 1;

	while((time_to_wait>0) && (no_lines_to_display>0)) {
	    if ((fp = popen(adb_cmd_str, "r")) == NULL) {
	          printf("Error opening pipe!\n");
	          return -1;
	    }
	    while (fgets(msg_str, MSG_STR_LEN, fp) != NULL) {
	           if(check_for_tag(msg_str, msg_tag) == TAG_MATCHING) {
	               if(no_lines_to_display > 0) {
	                   printf("OUTPUT: %s", msg_str);
	                   no_lines_to_display--;
	               } else {
	                   break;
	               }
	           }
	           fflush(stdout);
	           fflush(stdin);
	           fflush(fp);
	     }
	    pclose(fp);
	     //while ((getchar()) != '\n');
	     memset(msg_str, 0, MSG_STR_LEN);
         clear_logcat_msg(msg_tag);
	     if(no_lines_to_display<=0)
	         break;
	     sleep(1);
	     time_to_wait--;
	}

	return 0;



	if ((fp = popen(msg_str, "r")) == NULL) {
	      printf("Error opening pipe!\n");
	      return -1;
	}
	//--- Look for the logcat message with tag


	while(time_to_wait > 0) {
        while (fgets(msg_str, MSG_STR_LEN, fp) != NULL) {
            printf("LinesToGet: %d  Msg: %s \n", no_lines_to_display, msg_str);
           if(check_for_tag(msg_str, msg_tag) == TAG_MATCHING)
               if(no_lines_to_display > 0) {
                   printf("OUTPUT: %s", msg_str);
                   no_lines_to_display--;
               } else {
                   break;
               }
        }
        time_to_wait --;
        sleep(1);
	}

	 pclose(fp);
 
	return 0;
}

/*********************************************************************************
*********************************************************************************/
int hndlr_start_scan(int index, int argc, char *argv[])
{
#ifdef ENABLE_DBG
	printf("In hndlr_start_scan, looking for TAG: %s\n", cmd_tbl[index].msg_tag);
#endif
    if(argc != 4) {
         printf("Invalid Param for hndlr_start_scan \n");
         print_usage();
         return -1;
     }
    int timeout   = atoi(argv[2]);
    int num_line  = atoi(argv[3]);
	issue_adb_cmd_and_wait_for_response("start_scan", NULL, cmd_tbl[index].msg_tag, timeout, num_line);

	return 0;
}

/*********************************************************************************
*********************************************************************************/
int hndlr_stop_scan(int index, int argc, char *argv[])
{
#ifdef ENABLE_DBG
	printf("In hndlr_stop_scan, looking for TAG: %s\n", cmd_tbl[index].msg_tag);
#endif
    if(argc != 4) {
         printf("Invalid Param for hndlr_start_scan \n");
         print_usage();
         return -1;
     }
    int timeout   = atoi(argv[2]);
    int num_line  = atoi(argv[3]);
    issue_adb_cmd_and_wait_for_response("stop_scan", NULL, cmd_tbl[index].msg_tag, timeout, num_line);
	
	return 0;
}

/*********************************************************************************
*********************************************************************************/
int hndlr_ls_scan(int index, int argc, char *argv[])
{
#ifdef ENABLE_DBG
	printf("In hndlr_ls_scan, looking for TAG: %s\n", cmd_tbl[index].msg_tag);
#endif
    if(argc != 4) {
         printf("Invalid Param for hndlr_start_scan \n");
         print_usage();
         return -1;
     }
    int timeout   = atoi(argv[2]);
    int num_line  = atoi(argv[3]);
    issue_adb_cmd_and_wait_for_response("lsscan", NULL, cmd_tbl[index].msg_tag, timeout, num_line);
	
	return 0;
}

/*********************************************************************************
*********************************************************************************/
int do_connect_disconnect_pair_unpair(int index, int argc, char *argv[], char *parir_unpair)
{
    char cmd_param[CMD_STR_LEN];

    if(argc != 6) {
         printf("Invalid Param for do_pair_unpair \n");
         print_usage();
         return -1;
     }
    int timeout   = atoi(argv[2]);
    int num_line  = atoi(argv[3]);
    if((argv[4][0] != 'A') && (argv[4][0] != 'N')) {
        printf("Invalid Param type should be <A/N> for Address/Name \n");
        print_usage();
        return -1;
    }
    snprintf(cmd_param, CMD_STR_LEN, "%c %s",argv[4][0], argv[5] );
    issue_adb_cmd_and_wait_for_response(parir_unpair, cmd_param, cmd_tbl[index].msg_tag, timeout, num_line);

    return 0;

}
/*********************************************************************************
 * pair <address from the scan result>
*********************************************************************************/
int hndlr_start_pair(int index, int argc, char *argv[])
{

#ifdef ENABLE_DBG
	printf("In hndlr_start_pair, looking for TAG: %s\n", cmd_tbl[index].msg_tag);
#endif
	return do_connect_disconnect_pair_unpair(index, argc, argv, "pair");

}

/*********************************************************************************
*********************************************************************************/
int hndlr_drop_pair(int index, int argc, char *argv[])
{
#ifdef ENABLE_DBG
	printf("In hndlr_drop_pair, looking for TAG: %s\n", cmd_tbl[index].msg_tag);
#endif
    return do_connect_disconnect_pair_unpair(index, argc, argv, "unpair");
 }

/*********************************************************************************
*********************************************************************************/
int hndlr_ls_pair(int index, int argc, char *argv[])
{
#ifdef ENABLE_DBG
	printf("In hndlr_ls_pair, looking for TAG: %s\n", cmd_tbl[index].msg_tag);
#endif
	
    if(argc != 4) {
         printf("Invalid Param for hndlr_ls_pair \n");
         print_usage();
         return -1;
     }
    int timeout   = atoi(argv[2]);
    int num_line  = atoi(argv[3]);
    issue_adb_cmd_and_wait_for_response("lspair", NULL, cmd_tbl[index].msg_tag, timeout, num_line);

	return 0;
}

/*********************************************************************************
*********************************************************************************/
int hndlr_start_connect(int index, int argc, char *argv[])
{
#ifdef ENABLE_DBG
	printf("In hndlr_start_connect, looking for TAG: %s\n", cmd_tbl[index].msg_tag);
#endif
	return do_connect_disconnect_pair_unpair(index, argc, argv, "connect");
	
	return 0;
}

/*********************************************************************************
*********************************************************************************/
int hndlr_drop_connect(int index, int argc, char *argv[])
{
#ifdef ENABLE_DBG
	printf("In hndlr_drop_connect, looking for TAG: %s\n", cmd_tbl[index].msg_tag);
#endif
    return do_connect_disconnect_pair_unpair(index, argc, argv, "disconnect");
	
	return 0;
}

/*********************************************************************************
*********************************************************************************/
int hndlr_indication(int index, int argc, char *argv[])
{
#ifdef ENABLE_DBG
	printf("In hndlr_indication, looking for TAG: %s\n", cmd_tbl[index].msg_tag);
#endif
    if(argc != 4) {
         printf("Invalid Param for hndlr_ls_pair \n");
         print_usage();
         return -1;
     }
    int timeout   = atoi(argv[2]);
    int num_line  = atoi(argv[3]);
    issue_adb_cmd_and_wait_for_response("indication", NULL, cmd_tbl[index].msg_tag, timeout, num_line);
	
	return 0;
}

/*********************************************************************************
*********************************************************************************/
int hndlr_start_app(int index, int argc, char *argv[])
{
    char msg_str[MSG_STR_LEN];

     snprintf(msg_str, MSG_STR_LEN, "adb shell \"am start -n %s/%s.%s\"", MODULE_NAME, MODULE_NAME, START_ACTIVITY);
     printf("CMD: %s ", msg_str);
     issue_system_cmd(msg_str, 1);

 #ifdef ENABLE_DBG
     printf("D-issue_adb_cmd_and_wait_for_response Command %s\n", msg_str);
 #endif
     //issue_system_cmd(msg_str, 0);

}

/*********************************************************************************
*********************************************************************************/
static void print_usage()
{
    int cmd_len = sizeof(cmd_tbl)/sizeof(cmd_lkt_t);
    printf("============ Command Usage ==============\n");
    for(int i=0; i<cmd_len; i++) {
        printf("<cmd> %16s  %s \n", cmd_tbl[i].cmd, cmd_tbl[i].help_str);
    }
}



