int fact(int n)
{
	int f;

	if(n <= 0)
	{
		return 1;
	}


	f = fact(n - 1);
	
	return f * n;
}

void main()
{
	int m;
	int n;
	printf("Input n:");
	scanf(n);
	m = fact(10);
	printf(m);
}
