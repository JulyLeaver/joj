/*
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
*/

#include <bits/stdc++.h>

/*
#define MAX 1000000
int dp[MAX];
int min(int a, int b) { return a < b ? a : b; }
*/

//int dp[100];
int main()
{
    /*
    int N;
    scanf("%d", &N);
    dp[0] = 0;
    dp[1] = 1;
    for(int i = 2; i <= N; ++i)
    {
	dp[i] = 0x7FFFFFFF;
	for(int j = 1; j * j <= i; ++j) dp[i] = min(dp[i], dp[i - j * j] + 1);
    }
    printf("%d                             \n", dp[N]);
    */

    /*
    char cmdS[64];
    sprintf(cmdS, "cat /proc/%d/status | grep VmSize", getpid());
    system(cmdS);
    */

    int* p = (int*) malloc(4 * 33554432);
    free(p);

    /*
    int N;
    scanf("%d", &N);
    int* p = (int*) malloc(sizeof(int) * N);
    free(p);
    */
    return 0;
}
