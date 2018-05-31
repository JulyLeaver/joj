#include <iostream>
#include <algorithm>

using namespace std;

const int MAX = 100000 + 1;
int d[MAX] = {0,1};

int main()
{
    ios::sync_with_stdio(false); cin.tie(0);
    int N;
    cin >> N;
    for(int i = 2; i <= N; ++i) 
    {
	d[i] = 0x7FFFFFFF;
	for(int j = 1; j * j <= i; ++j) d[i] = min(d[i], d[i - j * j] + 1);
    }
    cout << d[N] << '\n';
    return 0;
}
