int fact(int n) {
	if (n < 1) {
		return 1;
	}
	return n * fact(n - 1);
}
void main()
{
	int n;
	int m;
	scanf(n);
	m = 1;
	while (n > 0) {
		m = m * n;
		n = n - 1;
	}
	printf(m);
}
