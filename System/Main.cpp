/*
         BB  .BQ  PBB     BB   RB.    rQS     gBBBBB:           7BBBBBS     BBr    iBK  iBP     .BQ  PBQ     BB   QBBBBBB         YBJ  7Bv     QB   QBBQBBBi       :BBBBBQQ    BBBBBBB            
         BB  .BB  gBBB    QB   BB.    7BP   uBBi  .gBB         BBX   7BBi   BBBr   rBg  iBg     .BB  RBBB    BB   BB              uBI  uB1     BB   BB.  .KBB     BBb:   :i    BB                 
         BB   BB  bBJQB   BQ   QB     :QS  :BB      YQd       BB:      BB   BBqBr  :BK  :Qd     .QB  dBYBB   QB   BQ              YBj  7Bv     BB   BB     .BB   BB            BB                 
         BB  .BB  PB  BB  BB   QBBBBBQBBU  gBi       BB       BB       UBY  BB gBi :Qq  :BP     .BB  EB  BB  BB   QBQBQBQ         vBs  vBv     BB   BB      BB  iBE   7MbXU.   BBgBBBI            
         BB   BB  EB. .BB BB   RBr.::.uBI  bBi       QB       BB       5Br  BB  BB. BX  :BP     .BB  EB. :BQ BB   BB:.:::         vBJ  YBr     BB   BB.     BB  :QQ   :jvQBI   BB .:i.            
         BB  .BB  dB:  :BRBB   BB     :B5  .BB      qB2       QBr      BB   BB   BBKQq  :B5     .BQ  DB:  :BgBB   QB              rB1  7Bq     BB   BB     PQK   BB:     :Qs   BB                 
       .iBB  .BB  BBi   :BBB   BB.    vBE   7QBu..rBBE         BBQi.:SBB.   BB    BBBD  rBB ..: .BB  BB:   :BQB   BQ: ..:       :.BBi   BBZ..iBB7   BBr iSBB5     BBB7...BB2   BB ....            
      7BQb   .BE  2B:    :BB   PB.    iBL     JBQBQb            .DQBBB7     BQ     EBs  :BBBQBB. BP  5B:    :BQ   qBBBBBB:      BBBr     :gBBBX     PBBBBgr        .5BBBBMu    BBBBBBB            

 
    JINHO ONLIME JUDGE
    2018/05/08 ~ 2018/05/12(v1.0)
 */

#include <iostream>
#include <algorithm>
#include <cstdio>
#include <string>
#include <unistd.h>
#include <cstring>
#include <assert.h>
#include <sys/stat.h>
#include <sys/ptrace.h>
#include <sys/wait.h>
#include <sys/types.h>
#include <sys/time.h>
#include <sys/resource.h>
#include <sys/user.h>

using namespace std;

enum class LANG
{
    C,
    CPP11
};
enum class RESULT
{
    AC,
    WA,
    TLE,
    MLE,
    RTE,
    NONE
};

const char *userBinFileName = "user.bin";
const char *userOutFileName = "user.out";

/*
user_id: 채점 시스템에 접속한 고유한 ID(IP 사용)
userFileName: 유저가 제출한 소스 코드 파일 이름
problemNumber: 문제 번호
*/
string user_id, userFileName, problemNumber;
LANG lang;

/*
return
    0: 컴파일 성공
    non-zero: 컴파일 실패
*/
int compile()
{
    int pid = fork();
    const char **compileCMD = nullptr;
    if (pid == 0)
    {
        string file = user_id + '/' + userFileName;
        string bin = user_id + '/' + userBinFileName;
        switch (lang)
        {
        case LANG::C:
            compileCMD = new const char *[5] { "gcc", file.c_str(), "-o", bin.c_str(), nullptr };
            break;
        case LANG::CPP11:
            compileCMD = new const char *[6] { "g++", "-std=c++11", file.c_str(), "-o", bin.c_str(), nullptr };
            break;
        }
        execvp(compileCMD[0], (char *const *)compileCMD);
    }
    int status = 0;
    waitpid(pid, &status, 0);
    delete[] compileCMD;
    /*
       WIFEXITED(status)는 자식이 정상적으로 종료 되었다면 non-zero이다. 
       WEXITSTATUS(status)는 인코딩된 status값을 다시 디코딩을 하는 것이다.
       어떻게 해야 비정상 종료가 되는 지 아직 모르겠다.
     */
    return WIFEXITED(status) ? WEXITSTATUS(status) : -1;
}

int getUseMemoryMB(const int childPid)
{
    char path[32];
    sprintf(path, "/proc/%d/status", childPid); // kb
    FILE *p = fopen(path, "r");
    if (!p)
        return 0;
    char buf[128];
    int ret = 0;
    const char *t = "VmPeak:";
    const int s = strlen(t);
    while (true)
    {
        fgets(buf, 128, p);
        if (feof(p))
            break;
        if (!strncmp(buf, t, s))
        {
            for (int i = s, bs = strlen(buf); i < bs; ++i)
            {
                if ('0' > buf[i] || buf[i] > '9')
                    continue;
                ret *= 10;
                ret += buf[i] - '0';
            }
            break;
        }
    }
    fclose(p);
    return ret / 1024;
}

RESULT running(const string &problemPath, int tcNumber, int limitSeconds, int limitMemoryMB, long *runningMilliseconds)
{
    int pid = fork();
    if (pid == 0)
    {
        string inPath = problemPath + to_string(tcNumber) + ".in";
        string outPath = user_id + '/' + userOutFileName;

        freopen(inPath.c_str(), "r", stdin);
        freopen(outPath.c_str(), "w", stdout);

        string run = "./";
        run += user_id + '/' + userBinFileName;

        struct rlimit rlim;

        rlim.rlim_cur = limitSeconds;
        rlim.rlim_max = limitSeconds;
        setrlimit(RLIMIT_CPU, &rlim);

        /*
	    getrlimit(RLIMIT_AS, &rlim);
	    rlim.rlim_cur = limitMemory / 2 * 3;
	    setrlimit(RLIMIT_AS, &rlim);
	    */

        ptrace(PTRACE_TRACEME, 0, nullptr, nullptr);
        execl(run.c_str(), run.c_str(), nullptr);
    }
    struct rusage ruse;
    struct user_regs_struct reg;
    int status = 0;
    int i = 0;
    int maxUseMemoryMB = 0;
    while (true)
    {
        wait4(pid, &status, 0, &ruse);
        ptrace(PTRACE_GETREGS, pid, nullptr, &reg);

        maxUseMemoryMB = max(maxUseMemoryMB, getUseMemoryMB(pid));
        if (limitMemoryMB < maxUseMemoryMB)
        {
            ptrace(PTRACE_KILL, pid, nullptr, nullptr);
            return RESULT::MLE;
        }
        if (WIFEXITED(status))
        {
            //cout << "정상 종료, WEXITSTATUS = " << WEXITSTATUS(status) << '\n';
            break;
        }
        if (WIFSIGNALED(status))
        {
            //cout << "시그널 발생, WTERMSIG = " << WTERMSIG(status) << '\n';
            switch (WTERMSIG(status))
            {
            case SIGKILL:
            case SIGXCPU:
                return RESULT::TLE;
            }
            return RESULT::RTE;
        }
        ptrace(PTRACE_SYSCALL, pid, nullptr, nullptr);
    }
    *runningMilliseconds = ruse.ru_utime.tv_sec * 1000 + ruse.ru_utime.tv_usec / 1000;
    *runningMilliseconds += ruse.ru_stime.tv_sec * 1000 + ruse.ru_stime.tv_usec / 1000;
    return RESULT::NONE;
}

/*
s1: 모범 답안(조건 양식에 맞다고 가정)
s2: 출제자 답안
 */
bool cmp(const char *s1, const char *s2)
{
    const int s1L = strlen(s1), s2L = strlen(s2);
    const int m = max(s1L, s2L);
    for (int i = 0; i < m; ++i)
    {
        const bool s2T = i >= s2L ? true : (s2[i] == '\n' || s2[i] == ' ' || s2[i] == '\0');
        if (i < s1L && s1[i] == '\n' && s2T)
            continue;
        if (i >= s1L && s2T)
            continue;
        if (s1[i] != s2[i])
            return false;
    }
    return true;
}

RESULT grading()
{
    string problemPath = string("../") + "Problems/" + problemNumber + '/';
    int tc, limitSeconds, limitMemoryMB;
    FILE *ex = fopen((problemPath + "ex").c_str(), "r"); // string 임시 객체 사라지지 않나? 다음 행에서 삭제 되나?
    /*
       각 문제의 ex 파일에는 차례대로 
       (테스트 케이스 수) (제한시간, 초) (제한 메모리(MB)
     */
    fscanf(ex, "%d %d %d", &tc, &limitSeconds, &limitMemoryMB);
    fclose(ex);

    cout << "문제 번호: " << problemNumber << ", 채점 시작" << '\n';
    cout << "테스트 케이스 수: " << tc << '\n';
    cout << "제한 시간: " << limitSeconds << "초" << '\n';
    cout << "제한 메모리: " << limitMemoryMB << "mb" << '\n';
    cout << '\n';

    limitMemoryMB += 20; // <bits/stdc++.h> 헤더 추가할 경우 15MB 정도 먹는다. 어쩔 수 없이 정확한 측정은 불가. +15mb 오차

    long runningMilliseconds;
    for (int i = 1; i <= tc; ++i)
    {
        cout << "채점 중... " << (double)i / tc * 100 << '%' << '\n';

        RESULT runningResult = running(problemPath, i, limitSeconds, limitMemoryMB, &runningMilliseconds);
        // RESULT::NONE 이라는 것은 아무 문제 없이 실행은 되었다. 즉 답안지 채점만 하면 된다는 뜻.
        if (runningResult != RESULT::NONE)
            return runningResult;
        FILE *outF = fopen((problemPath + to_string(i) + ".out").c_str(), "r");
        FILE *userOutF = fopen((user_id + '/' + userOutFileName).c_str(), "r");
        const int OUT_MAX = 32; // 출력 파일의 한 라인에 30자 넘으면 안됨
        char out[OUT_MAX], userOut[OUT_MAX];
        RESULT gradingResult = RESULT::AC;
        while (true)
        {
            fgets(out, OUT_MAX, outF);
            fgets(userOut, OUT_MAX, userOutF);

            // 코드로 넣으면 마지막에 개행이 안들어가는데
            // gedit 열어서 개행 안넣어도 개행이 들어간다...
            // 뭐지..?

            int outEOF = feof(outF), userOutEOF = feof(userOutF);
            if (outEOF && userOutEOF)
                break;

            const bool cmpR = cmp(out, userOut);
            if (cmpR)
                break;

            if (!cmpR || outEOF || userOutEOF)
            {
                gradingResult = RESULT::WA;
                break;
            }
        }
        fclose(outF);
        fclose(userOutF);
        if (gradingResult != RESULT::AC)
            return RESULT::WA;
        cout << "실행 시간: " << runningMilliseconds << "ms" << '\n';
        cout << '\n';
    }
    return RESULT::AC;
}

/*
JOJ
    Problems
	1000
	1001
	1002
	...
    System // 채점 프로그램이 실행 되는 경로
	Main.cpp
	run
    Server
    Client

 argv[1] = user_id(using IP, ex: 192.168.0.1)
 argv[2] = file name(ex: test.cpp)
 argv[3] = problem number(ex: 1001)

 모범 답에는 절대로 답 마지막에 아무것도 없거나 개행만 존재해야 한다.
 출제자의 답안지에는 답 마지막에 개행 또는 띄어쓰기 외 존재하면 절대 안된다.
 */
int main(int argc, char *argv[])
{
    // Init
    if (argc != 4)
    {
        cout << "인자 오류" << '\n';
        exit(0);
    }
    user_id = argv[1];
    userFileName = argv[2];
    problemNumber = argv[3];
    lang = (userFileName.back() == 'c' || userFileName.back() == 'C') ? LANG::C : LANG::CPP11;

    // 서버 프로그램에서 user_id로 된 폴더를 만들어 놓을 것, 또한 소스 파일도 user_id로 된 폴더 안에 있어야 한다.
    //if(access(user_id.c_str(), R_OK | W_OK) == -1 && mkdir(user_id.c_str(), 0777) == -1) exit(0);

    /*
    cout << "user_id: " << user_id << endl;
    cout << "userFileName: " << userFileName << endl;
    cout << "userBinFileName: " << userBinFileName << endl;
    cout << "userOutFileName: " << userOutFileName << endl;
    cout << "problemNumber: " << problemNumber << endl;
    cout << "lang: " << (int) lang << endl;
    */

    // Compile
    if (compile())
    {
        cout << "compile error" << '\n';
        exit(0);
    }
    else
        cout << "compile success" << '\n';

    cout << '\n';

    // Grading
    RESULT gradingResult = grading();
    string resultS;
    int r = 0;
    switch (gradingResult)
    {
    case RESULT::AC:
        resultS = "맞았습니다.";
        r = 1;
	break;
    case RESULT::WA:
        resultS = "틀렸습니다.";
        break;
    case RESULT::TLE:
        resultS = "시간 초과";
        break;
    case RESULT::MLE:
        resultS = "메모리 초과";
        break;
    case RESULT::RTE:
        resultS = "런타임 에러";
        break;
    }
    cout << resultS << '\n';

    // Destroy

    return r;
}
