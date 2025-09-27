// int getint() {
//     int t;
//     scanf("%d", &t);
//     while(getchar() != '\n');
//     return t;
// }
const int G_CONST1 = 7, G_CONST2 = 8, G[2] = {1, 2}; // 全局常量

// 函数：static局部变量+数组遍历
int count_arr(int arr[], int len)
{
    static int cnt = 0; // static局部变量（仅初始化1次）
    cnt = cnt + 1;
    int sum = 0;
    for (; len > 0; len = len - 1)
    { // for循环缺省初始化部分
        sum = sum + arr[len - 1];
    }
    printf("cnt=%d\n", cnt); // 嵌入printf（算写语句）
    return sum;
}

int main()
{
    printf("22241098\n");                  // 1. 输出学号
    int n = getint();                      // 读入input3.txt的4
    int arr[4] = {n, n * 2, n * 3, n * 4}; // 数组初始化

    printf("arr[0]=%d\n", arr[0]);                // 2. 数组元素
    printf("count_arr1=%d\n", count_arr(arr, 4)); // 3. 第一次调用（cnt=1）

    int val;
    val = getint();                         // 读入input3.txt的2
    arr[1] = val;                           // 修改数组元素
    printf("modified_arr[1]=%d\n", arr[1]); // 4. 修改后元素

    printf("count_arr2=%d\n", count_arr(arr, 4)); // 5. 第二次调用（cnt=2）

    // for循环缺省条件部分
    int i = 0;
    for (;; i = i + 1)
    {
        if (i >= 3)
            break;
        arr[i] = arr[i] + 1;
    }
    printf("arr[2]=%d\n", arr[2]); // 6. 循环修改后元素

    printf("count_arr3=%d\n", count_arr(arr, 4)); // 7. 第三次调用（cnt=3）

    printf("i=%d\n", i);                     // 8. 循环变量值
    printf("arr[3]-val=%d\n", arr[3] - val); // 9. 数组与变量运算
    printf("%d\n", val);                     // 10. 相等性运算
    return 0;
}